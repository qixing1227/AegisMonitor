<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  variants: {
    type: Array,
    required: true
  },
  labels: {
    type: Object,
    required: true
  },
  current: {
    type: String,
    required: true
  },
  state: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['change'])

const isProduction = import.meta.env.PROD
const currentIndex = computed(() => props.variants.indexOf(props.current))
const currentLabel = computed(() => `${props.current} - ${props.labels[props.current]}`)

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

function move(offset) {
  const nextIndex = (currentIndex.value + offset + props.variants.length) % props.variants.length
  emit('change', props.variants[nextIndex])
}

function handleKeydown(event) {
  const target = event.target
  const isTyping =
    target instanceof HTMLElement &&
    (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)
  if (isTyping) {
    return
  }
  if (event.key === 'ArrowLeft') {
    move(-1)
  }
  if (event.key === 'ArrowRight') {
    move(1)
  }
}
</script>

<template>
  <aside v-if="!isProduction" class="prototype-switcher" aria-label="Prototype variant switcher">
    <button class="prototype-switcher__button" type="button" title="Previous variant" @click="move(-1)">
      <ChevronLeft :size="18" aria-hidden="true" />
    </button>
    <div class="prototype-switcher__state">
      <strong>{{ currentLabel }}</strong>
      <span>
        {{ state.source }} | hosts {{ state.hosts }} | online {{ state.online }} |
        alerts {{ state.alerts }} | selected {{ state.selectedHostId }}
      </span>
    </div>
    <button class="prototype-switcher__button" type="button" title="Next variant" @click="move(1)">
      <ChevronRight :size="18" aria-hidden="true" />
    </button>
  </aside>
</template>
