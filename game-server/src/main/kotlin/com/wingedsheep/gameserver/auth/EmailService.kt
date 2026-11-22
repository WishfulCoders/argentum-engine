package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.config.AccountsProperties
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

/**
 * Sends the magic-link email. Uses the auto-configured [JavaMailSender] (Mailgun SMTP by default)
 * when mail credentials are configured; otherwise logs the link to the console so the whole login
 * flow is testable in local dev with no email account.
 *
 * The email is multipart: a styled HTML part (dark, on-brand, with a big sign-in button) plus a
 * plain-text fallback for clients that don't render HTML. Email HTML is deliberately old-school —
 * inline styles, table layout, no external CSS or images — because mail clients (Outlook especially)
 * ignore `<style>` blocks, flexbox, and `background-clip` text gradients.
 *
 * The actual SMTP send runs on a background thread: connecting to the mail host can take seconds (or
 * stall on the configured timeout if it's unreachable), and the sign-in request must not block on it
 * — the login token is already persisted by the time we're called, and `request-login` deliberately
 * returns 200 regardless of delivery. Send failures are logged, not surfaced to the caller.
 */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class EmailService(
    private val props: AccountsProperties,
    private val mailSender: ObjectProvider<JavaMailSender>,
    @Value("\${spring.mail.username:}") private val mailUsername: String,
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    private val sendExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "magic-link-mailer").apply { isDaemon = true }
    }

    /** Whether a magic link actually leaves this server; false means it is only logged. */
    val canSend: Boolean get() = mailUsername.isNotBlank() && mailSender.ifAvailable != null

    fun sendMagicLink(toEmail: String, link: String) {
        if (!canSend) {
            logger.warn(
                "Mail not configured (set MAIL_USERNAME/MAIL_PASSWORD) — magic link for {} is: {}",
                toEmail, link,
            )
            return
        }
        val sender = mailSender.ifAvailable ?: return
        val ttl = props.auth.loginTokenTtlMinutes
        val mime = sender.createMimeMessage()
        MimeMessageHelper(mime, true, "UTF-8").apply {
            setFrom(props.auth.fromEmail)
            setTo(toEmail)
            setSubject("Your Argentum magic link 🧙")
            // setText(plain, html) — both parts; clients pick what they can render.
            setText(buildTextBody(link, ttl), buildHtmlBody(link, ttl))
        }
        sendExecutor.submit {
            try {
                sender.send(mime)
                logger.info("Sent magic-link email to {}", toEmail)
            } catch (e: Exception) {
                // Don't fail the (already-200) login request; a misconfigured/unreachable mail host
                // must not stall sign-in. Log loudly so the operator can fix delivery.
                logger.error("Failed to send magic-link email to {}: {}", toEmail, e.message, e)
            }
        }
    }

    private fun buildTextBody(link: String, ttlMinutes: Long): String = buildString {
        appendLine("Argentum Engine")
        appendLine()
        appendLine("Your magic link is ready — cast it to sign in:")
        appendLine()
        appendLine(link)
        appendLine()
        appendLine("This link expires in $ttlMinutes minutes and can be used once.")
        appendLine("If you didn't request this, you can safely ignore this email.")
    }

    private fun buildHtmlBody(link: String, ttlMinutes: Long): String {
        val safeLink = htmlEscape(link)
        // Brand palette mirrors the web client: deep navy surfaces, metallic-silver wordmark,
        // electric-blue CTA. Solid colours only — gradient text doesn't survive email clients.
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="color-scheme" content="dark">
  <title>Your Argentum magic link</title>
</head>
<body style="margin:0; padding:0; background-color:#0a0a15; color:#dddddd; -webkit-font-smoothing:antialiased; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
  <div style="display:none; max-height:0; overflow:hidden; opacity:0;">Your single-use magic link to sign in to Argentum — expires in $ttlMinutes minutes.</div>
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#0a0a15;">
    <tr>
      <td align="center" style="padding:32px 16px;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px; background-color:#1a1a2e; border:1px solid #2a2a44; border-radius:16px; overflow:hidden;">
          <!-- Header -->
          <tr>
            <td align="center" style="padding:40px 40px 8px 40px;">
              <div style="font-size:28px; font-weight:700; letter-spacing:0.5px; color:#e8edf5;">Argentum<span style="color:#4fc3f7;"> Engine</span></div>
              <div style="margin-top:6px; font-size:13px; letter-spacing:2px; text-transform:uppercase; color:#8a93a8;">Magic: The Gathering</div>
            </td>
          </tr>
          <!-- Body -->
          <tr>
            <td style="padding:24px 40px 8px 40px;">
              <h1 style="margin:0 0 12px 0; font-size:22px; font-weight:600; color:#ffffff;">Your magic link is ready &#10024;</h1>
              <p style="margin:0; font-size:15px; line-height:1.6; color:#bcc4d4;">
                Tap the button below to cast it and sign in to Argentum. No password, no spellbook required.
              </p>
            </td>
          </tr>
          <!-- Button -->
          <tr>
            <td align="center" style="padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center" bgcolor="#2563eb" style="border-radius:10px;">
                    <a href="$safeLink" target="_blank" style="display:inline-block; padding:14px 38px; font-size:16px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:10px; background-color:#2563eb;">
                      Sign in to Argentum
                    </a>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <!-- Fallback link -->
          <tr>
            <td style="padding:0 40px 8px 40px;">
              <p style="margin:0; font-size:12px; line-height:1.5; color:#8a93a8;">
                Button not working? Copy and paste this link into your browser:
              </p>
              <p style="margin:6px 0 0 0; font-size:12px; line-height:1.5; word-break:break-all;">
                <a href="$safeLink" target="_blank" style="color:#4fc3f7; text-decoration:underline;">$safeLink</a>
              </p>
            </td>
          </tr>
          <!-- Divider -->
          <tr>
            <td style="padding:24px 40px 0 40px;">
              <div style="border-top:1px solid #2a2a44; line-height:1px; font-size:1px;">&nbsp;</div>
            </td>
          </tr>
          <!-- Footer -->
          <tr>
            <td style="padding:20px 40px 36px 40px;">
              <p style="margin:0; font-size:12px; line-height:1.6; color:#6f7890;">
                This link expires in <strong style="color:#9aa3b8;">$ttlMinutes minutes</strong> and can be used once.<br>
                If you didn't request this, you can safely ignore this email — no one can sign in without the link above.
              </p>
            </td>
          </tr>
        </table>
        <div style="margin-top:18px; font-size:11px; color:#5a6175;">Argentum Engine &middot; sent because someone requested a sign-in link</div>
      </td>
    </tr>
  </table>
</body>
</html>
""".trimIndent()
    }

    private fun htmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    @PreDestroy
    fun shutdown() {
        sendExecutor.shutdown()
    }
}
