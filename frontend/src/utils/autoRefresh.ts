export interface AutoRefreshOptions {
  intervalMs: number
  isHidden: () => boolean
  isPending: () => boolean
  refresh: () => void
}

export interface AutoRefreshController {
  start: () => void
  stop: () => void
  refreshNow: () => void
  resume: () => void
  handleVisibilityChange: () => void
}

export function createAutoRefreshController(options: AutoRefreshOptions): AutoRefreshController {
  let timer: ReturnType<typeof setInterval> | undefined

  function refreshNow() {
    if (options.isHidden() || options.isPending()) return
    options.refresh()
  }

  function stop() {
    if (!timer) return
    clearInterval(timer)
    timer = undefined
  }

  function start() {
    stop()
    timer = setInterval(refreshNow, options.intervalMs)
  }

  function resume() {
    if (options.isHidden()) return
    start()
    refreshNow()
  }

  function handleVisibilityChange() {
    if (!options.isHidden()) {
      resume()
    }
  }

  return {
    start,
    stop,
    refreshNow,
    resume,
    handleVisibilityChange
  }
}

export function usePageAutoRefresh(options: AutoRefreshOptions): AutoRefreshController {
  const controller = createAutoRefreshController(options)

  onMounted(() => {
    document.addEventListener('visibilitychange', controller.handleVisibilityChange)
    window.addEventListener('focus', controller.resume)
    window.addEventListener('online', controller.resume)
    window.addEventListener('pageshow', controller.resume)
    controller.start()
  })

  onBeforeUnmount(() => {
    controller.stop()
    document.removeEventListener('visibilitychange', controller.handleVisibilityChange)
    window.removeEventListener('focus', controller.resume)
    window.removeEventListener('online', controller.resume)
    window.removeEventListener('pageshow', controller.resume)
  })

  return controller
}
import { onBeforeUnmount, onMounted } from 'vue'
