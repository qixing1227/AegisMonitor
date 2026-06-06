export function createRefreshLoop(options) {
  const intervalMs = options.intervalMs
  const refresh = options.refresh
  const setIntervalFn = options.setInterval ?? globalThis.setInterval
  const clearIntervalFn = options.clearInterval ?? globalThis.clearInterval
  let timerId = null

  function start() {
    if (timerId !== null) {
      return
    }

    timerId = setIntervalFn(() => {
      void refresh()
    }, intervalMs)
  }

  function stop() {
    if (timerId === null) {
      return
    }

    clearIntervalFn(timerId)
    timerId = null
  }

  return {
    start,
    stop,
    get running() {
      return timerId !== null
    }
  }
}
