/**
 * `/help` — the Argentum wiki. Deep-linkable as `/help/<section>#<topic-id>`, which is what every
 * inline {@link HelpTip}'s "Read more" points at. Bare `/help` is the wiki's front page: every
 * section with its full topic list, a "start here" row, and an A–Z index of every topic.
 *
 * Content comes entirely from `src/help/topics.ts`; this file is layout only. What it adds on top
 * of rendering a section is the three things a 40-topic guide needs to be usable: a search box that
 * spans every section, a per-section topic index, and a copy-link button on each topic — the deep
 * links existed but there was no way to get one out of the page.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  HELP_SECTIONS,
  HELP_TOPICS,
  START_HERE_TOPIC_IDS,
  topicsInSection,
  topicById,
  searchTopics,
  sectionMeta,
  helpHref,
  type HelpSection,
  type HelpTopic,
} from '@/help/topics'
import { HelpTopicView } from '@/components/help/HelpTopicView'
import { PageShell, pageStyles } from '@/components/ui/PageShell'
import styles from './HelpPage.module.css'

/** How long a topic stays ringed after being jumped to, so you can see where you landed. */
const HIGHLIGHT_MS = 2200

function isSection(value: string | undefined): value is HelpSection {
  return HELP_SECTIONS.some((s) => s.id === value)
}

export function HelpPage() {
  const { section: sectionParam } = useParams<{ section?: string }>()
  const navigate = useNavigate()
  // No (valid) section in the URL is the wiki's front page.
  const section: HelpSection | null = isSection(sectionParam) ? sectionParam : null

  const [query, setQuery] = useState('')
  const searchRef = useRef<HTMLInputElement>(null)
  // `#root` is overflow:hidden for the game board, so the page — not the window — is what scrolls.
  const pageRef = useRef<HTMLDivElement>(null)
  const [highlighted, setHighlighted] = useState<string | null>(null)

  const trimmed = query.trim()
  const results = useMemo(() => (trimmed ? searchTopics(trimmed) : null), [trimmed])
  const sectionTopics = useMemo(() => (section ? topicsInSection(section) : []), [section])
  const topics = results ?? sectionTopics
  const isHome = !results && section === null

  const index = section ? HELP_SECTIONS.findIndex((s) => s.id === section) : -1
  const previous = index > 0 ? HELP_SECTIONS[index - 1] : undefined
  const next = section === null
    ? HELP_SECTIONS[0]
    : index < HELP_SECTIONS.length - 1 ? HELP_SECTIONS[index + 1] : undefined

  /** Ring a topic for a moment after scrolling to it, then let it fade back. */
  const flag = useCallback((id: string) => {
    setHighlighted(id)
    window.setTimeout(() => setHighlighted((current) => (current === id ? null : current)), HIGHLIGHT_MS)
  }, [])

  const scrollToTopic = useCallback((id: string) => {
    // One frame is not enough when the section changed — the topic has to be in the DOM first.
    window.setTimeout(() => {
      document.getElementById(id)?.scrollIntoView({ block: 'start', behavior: 'smooth' })
      flag(id)
    }, 60)
  }, [flag])

  // Honour the #topic-id fragment once the section has rendered.
  useEffect(() => {
    const id = window.location.hash.slice(1)
    if (!id) return
    const timer = window.setTimeout(() => {
      document.getElementById(id)?.scrollIntoView({ block: 'start', behavior: 'smooth' })
      setHighlighted(id)
      window.setTimeout(() => setHighlighted((current) => (current === id ? null : current)), HIGHLIGHT_MS)
    }, 50)
    return () => window.clearTimeout(timer)
  }, [section])

  // `/` focuses search the way it does on most docs sites; Esc gives the field back.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null
      const typing = target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA'
      if (e.key === '/' && !typing) {
        e.preventDefault()
        searchRef.current?.focus()
      } else if (e.key === 'Escape' && typing) {
        setQuery('')
        searchRef.current?.blur()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  /** Follow a topic link: switch section if needed, clear any search, scroll and ring it. */
  const goToTopic = useCallback((id: string) => {
    const target = topicById(id)
    if (!target) return
    setQuery('')
    navigate(`/help/${target.section}#${id}`)
    scrollToTopic(id)
  }, [navigate, scrollToTopic])

  const goToSection = (id: HelpSection | null) => {
    setQuery('')
    navigate(id ? `/help/${id}` : '/help')
    pageRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const searchBox = (
    <div className={styles.searchBox}>
      <svg className={styles.searchIcon} viewBox="0 0 16 16" aria-hidden="true">
        <circle cx="7" cy="7" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
        <path d="M10.5 10.5 L14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      </svg>
      <input
        ref={searchRef}
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search the guide"
        aria-label="Search help topics"
        className={styles.searchInput}
      />
      {trimmed
        ? (
          <button
            type="button"
            className={styles.searchClear}
            onClick={() => { setQuery(''); searchRef.current?.focus() }}
            aria-label="Clear search"
          >
            ✕
          </button>
          )
        : <kbd className={styles.searchHint}>/</kbd>}
    </div>
  )

  return (
    <div className={styles.page} ref={pageRef}>
      <PageShell title="Help" width="normal">
        <header className={styles.header}>
          <h1 className={pageStyles.h1}>Argentum Wiki</h1>
          <p className={pageStyles.lede}>
            Everything Argentum does, in one place: every way to play, every screen, every setting.
            It assumes you know Magic — it explains what this app does with the rules, not the rules
            themselves.{' '}
            <Link to="/learn" className={styles.headerLink}>New to Magic? Learn to play →</Link>
          </p>
        </header>

        <div className={styles.layout}>
          <nav className={styles.nav} aria-label="Help sections">
            {searchBox}
            <div className={styles.navSections}>
              <button
                type="button"
                className={`${styles.navItem} ${isHome ? styles.navItemActive : ''}`}
                aria-current={isHome ? 'page' : undefined}
                onClick={() => goToSection(null)}
              >
                <span className={styles.navItemHead}>
                  <span className={styles.navItemTitle}>Wiki home</span>
                  <span className={styles.navItemCount}>{HELP_TOPICS.length}</span>
                </span>
                <span className={styles.navItemBlurb}>Every section, every topic, and an A–Z index.</span>
              </button>
              {HELP_SECTIONS.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  className={`${styles.navItem} ${s.id === section && !results ? styles.navItemActive : ''}`}
                  aria-current={s.id === section && !results ? 'page' : undefined}
                  onClick={() => goToSection(s.id)}
                >
                  <span className={styles.navItemHead}>
                    <span className={styles.navItemTitle}>{s.title}</span>
                    <span className={styles.navItemCount}>{topicsInSection(s.id).length}</span>
                  </span>
                  <span className={styles.navItemBlurb}>{s.blurb}</span>
                </button>
              ))}
            </div>

            {/* Long sections are otherwise a wall — 14 topics under Game modes with no way to see
                what is coming. Hidden while searching, where the result list is already the index. */}
            {!results && sectionTopics.length > 1 && (
              <div className={styles.onThisPage}>
                <span className={styles.onThisPageLabel}>On this page</span>
                {sectionTopics.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    className={styles.onThisPageLink}
                    onClick={() => goToTopic(t.id)}
                  >
                    {t.title}
                  </button>
                ))}
              </div>
            )}
          </nav>

          <div className={styles.content}>
            {isHome ? <WikiHome onTopic={goToTopic} onSection={goToSection} /> : null}

            {isHome ? null : results
              ? (
                <div className={styles.sectionIntro}>
                  <h2 className={styles.sectionTitle}>
                    {results.length} {results.length === 1 ? 'result' : 'results'}
                  </h2>
                  <p className={styles.sectionBlurb}>
                    {results.length > 0
                      ? <>Topics matching “{trimmed}”, across every section.</>
                      : <>Nothing matches “{trimmed}”. Try a shorter query, or browse the sections.</>}
                  </p>
                </div>
                )
              : section && (
                <div className={styles.sectionIntro}>
                  <h2 className={styles.sectionTitle}>{sectionMeta(section).title}</h2>
                  <p className={styles.sectionBlurb}>{sectionMeta(section).blurb}</p>
                </div>
                )}

            <div className={styles.topics}>
              {topics.map((topic) => (
                <HelpTopicView
                  key={topic.id}
                  topic={topic}
                  className={`${styles.topicCard} ${highlighted === topic.id ? styles.topicCardFlagged : ''}`}
                  onNavigate={goToTopic}
                  headerAction={
                    <>
                      {results && (
                        <span className={styles.resultSection}>{sectionMeta(topic.section).title}</span>
                      )}
                      <CopyLinkButton topic={topic} />
                    </>
                  }
                />
              ))}
            </div>

            {!results && (previous || next || section) && (
              <div className={styles.pager}>
                {previous
                  ? (
                    <button type="button" className={styles.pagerLink} onClick={() => goToSection(previous.id)}>
                      <span className={styles.pagerDirection}>← Previous</span>
                      <span className={styles.pagerTitle}>{previous.title}</span>
                    </button>
                    )
                  : section
                    ? (
                      <button type="button" className={styles.pagerLink} onClick={() => goToSection(null)}>
                        <span className={styles.pagerDirection}>← Wiki home</span>
                        <span className={styles.pagerTitle}>Contents</span>
                      </button>
                      )
                    : <span />}
                {next && (
                  <button
                    type="button"
                    className={`${styles.pagerLink} ${styles.pagerLinkNext}`}
                    onClick={() => goToSection(next.id)}
                  >
                    <span className={styles.pagerDirection}>Next →</span>
                    <span className={styles.pagerTitle}>{next.title}</span>
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </PageShell>
    </div>
  )
}

/**
 * The front page: a "start here" row, every section with its whole topic list, and an A–Z index.
 * A wiki's contents page is the thing that tells you what exists before you know what to search for.
 */
function WikiHome({
  onTopic,
  onSection,
}: {
  onTopic: (id: string) => void
  onSection: (id: HelpSection) => void
}) {
  const startHere = START_HERE_TOPIC_IDS.map(topicById).filter((t): t is HelpTopic => !!t)
  const alphabetical = useMemo(
    () => [...HELP_TOPICS].sort((a, b) => indexKey(a.title).localeCompare(indexKey(b.title))),
    [],
  )
  const byLetter = useMemo(() => {
    const groups = new Map<string, HelpTopic[]>()
    for (const t of alphabetical) {
      const first = indexKey(t.title).charAt(0).toUpperCase()
      const letter = /[A-Z]/.test(first) ? first : '#'
      groups.set(letter, [...(groups.get(letter) ?? []), t])
    }
    return [...groups.entries()]
  }, [alphabetical])

  return (
    <div className={styles.home}>
      <section className={styles.homeBlock}>
        <h2 className={styles.sectionTitle}>Start here</h2>
        <div className={styles.startGrid}>
          {startHere.map((t) => (
            <button key={t.id} type="button" className={styles.startCard} onClick={() => onTopic(t.id)}>
              <span className={styles.startCardSection}>{sectionMeta(t.section).title}</span>
              <span className={styles.startCardTitle}>{t.title}</span>
              <span className={styles.startCardSummary}>{t.summary}</span>
            </button>
          ))}
        </div>
      </section>

      <section className={styles.homeBlock}>
        <h2 className={styles.sectionTitle}>Contents</h2>
        <div className={styles.contentsGrid}>
          {HELP_SECTIONS.map((s) => (
            <div key={s.id} className={styles.contentsCard}>
              <button type="button" className={styles.contentsTitle} onClick={() => onSection(s.id)}>
                {s.title}
                <span className={styles.navItemCount}>{topicsInSection(s.id).length}</span>
              </button>
              <p className={styles.contentsBlurb}>{s.blurb}</p>
              <ul className={styles.contentsList}>
                {topicsInSection(s.id).map((t) => (
                  <li key={t.id}>
                    <button type="button" className={styles.onThisPageLink} onClick={() => onTopic(t.id)}>
                      {t.title}
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className={styles.homeBlock}>
        <h2 className={styles.sectionTitle}>A–Z index</h2>
        <div className={styles.indexGrid}>
          {byLetter.map(([letter, group]) => (
            <div key={letter} className={styles.indexGroup}>
              <span className={styles.indexLetter}>{letter}</span>
              {group.map((t) => (
                <button key={t.id} type="button" className={styles.onThisPageLink} onClick={() => onTopic(t.id)}>
                  {t.title}
                </button>
              ))}
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}

/** Index titles by their first real word: "The deckbuilder" files under D, "⚡ Assay-ready" under A. */
function indexKey(title: string): string {
  return title.replace(/^[^A-Za-z0-9]+/, '').replace(/^(the|a|an)\s+/i, '')
}

/** Copies the topic's deep link. The links already worked; there was no way to obtain one. */
function CopyLinkButton({ topic }: { topic: HelpTopic }) {
  const [copied, setCopied] = useState(false)
  const url = new URL(helpHref(topic), window.location.origin).href

  const copy = () => {
    void navigator.clipboard?.writeText(url).then(
      () => {
        setCopied(true)
        window.setTimeout(() => setCopied(false), 1400)
      },
      () => { /* clipboard blocked — the anchor is still in the address bar after a click */ },
    )
  }

  return (
    <button
      type="button"
      className={`${styles.copyLink} ${copied ? styles.copyLinkCopied : ''}`}
      onClick={copy}
      title={`Copy a link to “${topic.title}”`}
      aria-label={`Copy a link to ${topic.title}`}
    >
      {copied ? 'Copied' : '#'}
    </button>
  )
}
