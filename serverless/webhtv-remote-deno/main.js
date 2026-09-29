import { handleRelayRequest } from './relay.js';
import { handlePlaybackSyncRequest, isPlaybackSyncPath } from './playback-sync.js';
import { createDenoKvPlaybackStore } from './playback-store.js';
import { handleHtmlRelayRequest } from './html-relay.js';

const playbackStore = createDenoKvPlaybackStore();

Deno.serve((request) => {
  const pathname = new URL(request.url).pathname;
  if (pathname === '/m3u8' || pathname === '/api/html-relay') return handleHtmlRelayRequest(request);
  if (isPlaybackSyncPath(pathname)) return handlePlaybackSyncRequest(request, playbackStore);
  return handleRelayRequest(request, {
    serverName: 'Deno Deploy Relay',
    playbackSync: playbackStore.isConfigured(),
    playbackPersistentStorage: playbackStore.persistent && playbackStore.isConfigured()
  });
});
