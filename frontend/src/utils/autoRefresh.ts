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
  let pendingRetryTimer: ReturnType<typeof setTimeout> | undefined

  function refreshNow() {
    if (options.isHidden()) return
    if (options.isPending()) {
      // 不并发请求，但不能因为一次较慢的响应错过整个刷新周期。
      // 请求结束后的下一次检查最多延后 50 ms。
      if (!pendingRetryTimer) {
        pendingRetryTimer = setTimeout(() => {
          pendingRetryTimer = undefined
          refreshNow()
        }, 50)
      }
      return
    }
    options.refresh()
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = undefined
    }
    if (pendingRetryTimer) {
      clearTimeout(pendingRetryTimer)
      pendingRetryTimer = undefined
    }
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
