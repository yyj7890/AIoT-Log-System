import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createAutoRefreshController } from './autoRefresh'

describe('createAutoRefreshController', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('refreshes at the configured interval while visible', () => {
    const refresh = vi.fn()
    const controller = createController({ refresh })

    controller.start()
    vi.advanceTimersByTime(3000)

    expect(refresh).toHaveBeenCalledTimes(3)
  })

  it('pauses while hidden and refreshes immediately when visible again', () => {
    let hidden = true
    const refresh = vi.fn()
    const controller = createController({ refresh, isHidden: () => hidden })

    controller.start()
    vi.advanceTimersByTime(3000)
    expect(refresh).not.toHaveBeenCalled()

    hidden = false
    controller.handleVisibilityChange()
    expect(refresh).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(1000)
    expect(refresh).toHaveBeenCalledTimes(2)
  })

  it('skips overlapping requests and recovers on the next interval', () => {
    let pending = true
    const refresh = vi.fn()
    const controller = createController({ refresh, isPending: () => pending })

    controller.start()
    vi.advanceTimersByTime(1000)
    expect(refresh).not.toHaveBeenCalled()

    pending = false
    vi.advanceTimersByTime(1000)
    expect(refresh).toHaveBeenCalledTimes(1)
  })

  it('restarts on focus or network recovery without creating duplicate timers', () => {
    const refresh = vi.fn()
    const controller = createController({ refresh })

    controller.start()
    controller.resume()
    controller.resume()
    expect(refresh).toHaveBeenCalledTimes(2)

    vi.advanceTimersByTime(1000)
    expect(refresh).toHaveBeenCalledTimes(3)

    controller.stop()
    vi.advanceTimersByTime(3000)
    expect(refresh).toHaveBeenCalledTimes(3)
  })
})

function createController(overrides: Partial<Parameters<typeof createAutoRefreshController>[0]> = {}) {
  return createAutoRefreshController({
    intervalMs: 1000,
    isHidden: () => false,
    isPending: () => false,
    refresh: () => undefined,
    ...overrides
  })
}
