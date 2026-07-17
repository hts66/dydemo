export interface SlideOptions {
  container: HTMLElement
  onSlideStart?: (direction: 'up' | 'down') => void
  onSlideMove?: (distance: number, direction: 'up' | 'down') => void
  onSlideEnd?: (direction: 'up' | 'down', distance: number, isSuccess: boolean) => void
  onSwitch?: (direction: 'up' | 'down') => void
  threshold?: number
  animationDuration?: number
}

export class SlideHandler {
  private container: HTMLElement
  private onSlideStart?: (direction: 'up' | 'down') => void
  private onSlideMove?: (distance: number, direction: 'up' | 'down') => void
  private onSlideEnd?: (direction: 'up' | 'down', distance: number, isSuccess: boolean) => void
  private onSwitch?: (direction: 'up' | 'down') => void

  private startY = 0
  private startX = 0
  private currentY = 0
  private startTime = 0
  private isDragging = false
  private isAnimating = false
  private hasMoved = false

  private threshold = 50
  private animationDuration = 300
  private velocityThreshold = 0.3

  private animationFrameId: number | null = null

  // bound handlers for proper removal
  private boundTouchStart: (e: PointerEvent) => void
  private boundTouchMove: (e: PointerEvent) => void
  private boundTouchEnd: (e: PointerEvent) => void
  private boundWheel: (e: WheelEvent) => void

  constructor(options: SlideOptions) {
    this.container = options.container
    this.onSlideStart = options.onSlideStart
    this.onSlideMove = options.onSlideMove
    this.onSlideEnd = options.onSlideEnd
    this.onSwitch = options.onSwitch

    if (options.threshold) this.threshold = options.threshold
    if (options.animationDuration) this.animationDuration = options.animationDuration

    this.boundTouchStart = this.slideTouchStart.bind(this)
    this.boundTouchMove = this.slideTouchMove.bind(this)
    this.boundTouchEnd = this.slideTouchEnd.bind(this)
    this.boundWheel = this.handleWheel.bind(this)

    this.bindEvents()
  }

  private bindEvents() {
    this.container.addEventListener('pointerdown', this.boundTouchStart, { passive: true })
    this.container.addEventListener('pointermove', this.boundTouchMove, { passive: false })
    this.container.addEventListener('pointerup', this.boundTouchEnd)
    this.container.addEventListener('pointercancel', this.boundTouchEnd)
    this.container.addEventListener('wheel', this.boundWheel, { passive: false })
  }

  private slideTouchStart(e: PointerEvent) {
    if (this.isAnimating) return

    this.startY = e.clientY
    this.startX = e.clientX
    this.currentY = this.startY
    this.startTime = Date.now()
    this.isDragging = true
    this.hasMoved = false

    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
      this.animationFrameId = null
    }
  }

  private slideTouchMove(e: PointerEvent) {
    if (!this.isDragging || this.isAnimating) return

    this.currentY = e.clientY
    const deltaY = this.currentY - this.startY
    const deltaX = e.clientX - this.startX

    // Horizontal scroll detection: if horizontal movement exceeds vertical, cancel slide
    if (!this.hasMoved && Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 15) {
      this.isDragging = false
      return
    }

    if (Math.abs(deltaY) < 5) return

    this.hasMoved = true
    e.preventDefault()

    const direction: 'up' | 'down' = deltaY > 0 ? 'down' : 'up'

    if (this.onSlideStart && Math.abs(deltaY) > 10) {
      this.onSlideStart(direction)
    }

    if (this.onSlideMove) {
      this.onSlideMove(deltaY, direction)
    }
  }

  private slideTouchEnd(_e: PointerEvent) {
    if (!this.isDragging || this.isAnimating) {
      this.isDragging = false
      return
    }

    this.isDragging = false

    if (!this.hasMoved) return

    const deltaY = this.currentY - this.startY
    const distance = Math.abs(deltaY)
    const direction: 'up' | 'down' = deltaY > 0 ? 'down' : 'up'
    const elapsedTime = Date.now() - this.startTime
    const screenHeight = window.innerHeight

    // Threshold: swipe distance > 1/3 screen OR swipe time < 150ms with meaningful distance
    const distanceThreshold = screenHeight / 3
    const isFastSwipe = elapsedTime < 150 && distance > 20
    const isLongSwipe = distance > distanceThreshold
    const isSuccess = isFastSwipe || isLongSwipe

    if (this.onSlideEnd) {
      this.onSlideEnd(direction, deltaY, isSuccess)
    }

    if (isSuccess && this.onSwitch) {
      this.onSwitch(direction)
    }
  }

  private handleWheel(e: WheelEvent) {
    e.preventDefault()

    if (this.isAnimating) return

    const direction: 'up' | 'down' = e.deltaY > 0 ? 'down' : 'up'

    if (this.onSwitch) {
      this.onSwitch(direction)
    }
  }

  public setAnimating(isAnimating: boolean) {
    this.isAnimating = isAnimating
  }

  public destroy() {
    this.container.removeEventListener('pointerdown', this.boundTouchStart)
    this.container.removeEventListener('pointermove', this.boundTouchMove)
    this.container.removeEventListener('pointerup', this.boundTouchEnd)
    this.container.removeEventListener('pointercancel', this.boundTouchEnd)
    this.container.removeEventListener('wheel', this.boundWheel)

    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
    }
  }
}
