import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore.ts'
import { useConnectName } from '@/store/useConnectName'
import { EntryCard } from './EntryCard'

/**
 * Entry point for the `/join/:lobbyId` deep link — the target of a lobby's QR code / share link.
 *
 * Lobby-kind agnostic: it always joins via `joinQuickGameLobby`, whose server handler delegates to
 * the tournament join handler when the id is a tournament lobby. So one link/QR covers Quick Game,
 * sealed/draft and tournament lobbies alike.
 *
 * Two cases, mirroring {@link TournamentEntryPage}:
 * 1. No name yet: show a name entry, connect, then auto-join once connected.
 * 2. A name already known (stored, or a signed-in account's display name): connect and auto-join
 *    without asking.
 *
 * Once any lobby state arrives, navigate to "/" where the normal lobby UI takes over.
 */
export function JoinLobbyPage() {
  const { lobbyId } = useParams<{ lobbyId: string }>()
  const navigate = useNavigate()

  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const connect = useGameStore((state) => state.connect)
  const joinAnyLobby = useGameStore((state) => state.joinQuickGameLobby)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const quickGameLobbyState = useGameStore((state) => state.quickGameLobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const lastError = useGameStore((state) => state.lastError)
  const sessionReplaced = useGameStore((state) => state.sessionReplaced)

  const { name: connectName, resolving: nameResolving } = useConnectName()
  const [playerName, setPlayerName] = useState(() => localStorage.getItem('argentum-player-name') || '')
  const [joining, setJoining] = useState(false)
  const hasJoinedRef = useRef(false)
  const hasConnectedRef = useRef(false)

  // Auto-connect anyone who already has a name — stored, or a signed-in account's display name.
  // Never while another tab/device owns the session — reconnecting would steal it back (mirrors
  // App.tsx / TournamentEntryPage).
  useEffect(() => {
    if (sessionReplaced) return
    if (connectName && connectionStatus === 'disconnected' && !hasConnectedRef.current) {
      hasConnectedRef.current = true
      connect(connectName)
    }
  }, [connectionStatus, connect, connectName, sessionReplaced])

  // Auto-join once connected (fires for both the returning user and the fresh name-entry path).
  useEffect(() => {
    if (connectionStatus === 'connected' && lobbyId && !hasJoinedRef.current) {
      hasJoinedRef.current = true
      setJoining(true)
      joinAnyLobby(lobbyId)
    }
  }, [connectionStatus, lobbyId, joinAnyLobby])

  // Navigate home once any lobby/tournament state is received.
  useEffect(() => {
    if (lobbyState || quickGameLobbyState || tournamentState) {
      navigate('/', { replace: true })
    }
  }, [lobbyState, quickGameLobbyState, tournamentState, navigate])

  const handleConnect = () => {
    if (playerName.trim()) {
      localStorage.setItem('argentum-player-name', playerName.trim())
      connect(playerName.trim())
    }
  }

  const errorMessage = lastError?.message?.toLowerCase().includes('lobby')
    || lastError?.message?.toLowerCase().includes('not found')
    ? lastError?.message
    : null

  // Only a visitor with no name at all is asked for one; everyone else is mid-connect above.
  const showNameEntry = !errorMessage && connectionStatus === 'disconnected' && !connectName && !nameResolving

  return (
    <EntryCard
      kicker="You're invited"
      title="Join a game"
      code={lobbyId}
      error={errorMessage}
      nameEntry={showNameEntry ? { value: playerName, onChange: setPlayerName, onSubmit: handleConnect, submitLabel: 'Join lobby' } : undefined}
      progressText={joining ? 'Joining lobby…' : 'Connecting…'}
    />
  )
}
