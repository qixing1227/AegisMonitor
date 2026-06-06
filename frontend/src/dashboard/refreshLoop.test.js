import assert from 'node:assert/strict'
import test from 'node:test'
import { createRefreshLoop } from './refreshLoop.js'

test('dashboard refresh loop repeats work and clears its timer', () => {
  const calls = []
  const cleared = []
  let scheduledCallback = null

  const loop = createRefreshLoop({
    intervalMs: 5000,
    refresh: () => calls.push('refresh'),
    setInterval: (callback, intervalMs) => {
      scheduledCallback = callback
      calls.push(`scheduled:${intervalMs}`)
      return 'timer-1'
    },
    clearInterval: (timerId) => cleared.push(timerId)
  })

  loop.start()
  scheduledCallback()
  loop.stop()

  assert.deepEqual(calls, ['scheduled:5000', 'refresh'])
  assert.deepEqual(cleared, ['timer-1'])
  assert.equal(loop.running, false)
})
