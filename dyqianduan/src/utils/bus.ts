const eventMap = new Map()

export const EVENT_KEY = {
  ITEM_PLAY: 'ITEM_PLAY',
  ITEM_STOP: 'ITEM_STOP',
  ITEM_TOGGLE: 'ITEM_TOGGLE',
  CURRENT_ITEM: 'CURRENT_ITEM',
  OPEN_COMMENTS: 'OPEN_COMMENTS',
  SHOW_SHARE: 'SHOW_SHARE',
  SINGLE_CLICK_BROADCAST: 'SINGLE_CLICK_BROADCAST',
  FOLLOW_STATUS_CHANGED: 'FOLLOW_STATUS_CHANGED'
}

export const on = (event: string, callback: Function) => {
  if (!eventMap.has(event)) {
    eventMap.set(event, [])
  }
  eventMap.get(event).push(callback)
}

export const off = (event: string, callback?: Function) => {
  if (!eventMap.has(event)) return
  if (callback) {
    eventMap.set(event, eventMap.get(event).filter((cb: Function) => cb !== callback))
  } else {
    eventMap.delete(event)
  }
}

export const emit = (event: string, ...args: any[]) => {
  if (!eventMap.has(event)) return
  eventMap.get(event).forEach((callback: Function) => {
    try {
      callback(...args)
    } catch (e) {
      console.error('Event callback error:', e)
    }
  })
}

export default { EVENT_KEY, on, off, emit }