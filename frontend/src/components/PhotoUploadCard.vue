<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { fileUrl } from '../api/client'
import Icon from './Icon.vue'

const props = defineProps({
  item: { type: Object, required: true }
})
const emit = defineEmits(['select', 'zoom'])

const cameraInput = ref(null)
const galleryInput = ref(null)
const previewUrl = ref('')
const error = ref('')

function pickCamera() {
  cameraInput.value?.click()
}
function pickGallery() {
  galleryInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (!file) return
  if (!file.type.startsWith('image/')) {
    error.value = '이미지 파일만 선택할 수 있습니다.'
    return
  }
  error.value = ''
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
  // 즉시 업로드하지 않고 선택된 파일만 부모로 전달 (전송 시 일괄 업로드)
  emit('select', { itemId: props.item.itemId, file })
}

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <div
    class="photo-card"
    :class="{ done: !!previewUrl, returned: item.status === 'RETURNED' }"
    :data-item-id="item.itemId"
  >
    <div class="head">
      <span class="step">
        <Icon v-if="previewUrl" name="check" :size="26" />
      </span>
      <span class="name">{{ item.name }}</span>
      <span class="status-chip" :class="previewUrl ? 'ok' : 'todo'">
        {{ previewUrl ? '완료' : '필요' }}
      </span>
    </div>

    <p v-if="item.description" class="desc">{{ item.description }}</p>

    <div v-if="item.status === 'RETURNED' && item.rejectReason" class="reject">
      <strong>반환 사유</strong>
      <span>{{ item.rejectReason }}</span>
    </div>

    <!-- 예시 사진(위) → 내 사진(아래) -->
    <div class="images">
      <div v-if="item.exampleImageUrl" class="thumb" @click="emit('zoom', fileUrl(item.exampleImageUrl))">
        <span class="thumb-label">예시</span>
        <img :src="fileUrl(item.exampleImageUrl)" alt="예시" />
      </div>
      <div v-if="previewUrl" class="thumb" @click="emit('zoom', previewUrl)">
        <span class="thumb-label uploaded">내 사진</span>
        <img :src="previewUrl" alt="선택한 사진" />
      </div>
    </div>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 10px">{{ error }}</div>

    <div class="actions">
      <button class="btn btn-ghost tap-btn" @click="pickCamera">
        <Icon name="camera" :size="22" />
        <span>{{ previewUrl ? '다시 촬영' : '카메라 촬영' }}</span>
      </button>
      <button class="btn btn-ghost tap-btn" @click="pickGallery">
        <Icon name="image" :size="22" />
        <span>사진첩</span>
      </button>
    </div>

    <input
      ref="cameraInput"
      type="file"
      accept="image/*"
      capture="environment"
      hidden
      @change="onFileChange"
    />
    <input ref="galleryInput" type="file" accept="image/*" hidden @change="onFileChange" />
  </div>
</template>

<style scoped>
.photo-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.photo-card.done {
  border-color: #86efac;
  box-shadow: inset 4px 0 0 var(--success);
}
.photo-card.returned {
  border-color: #fca5a5;
  background: #fffafa;
}
.head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  flex: 0 0 auto;
  color: #fff;
  background: #cbd5e1;
}
.photo-card.done .step {
  background: var(--success);
}
.name {
  font-size: 24px;
  font-weight: 700;
  flex: 1;
  min-width: 0;
}
.status-chip {
  font-size: 17px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 999px;
  flex: 0 0 auto;
}
.status-chip.ok {
  color: #15803d;
  background: #f0fdf4;
}
.status-chip.todo {
  color: var(--text-muted);
  background: #f1f5f9;
}
.desc {
  margin: 0 0 12px;
  font-size: 21px;
  line-height: 1.5;
  color: var(--text-muted);
}
.reject {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--danger-bg);
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
  font-size: 21px;
}
.reject strong {
  color: var(--danger);
  font-size: 18px;
}
.images {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}
.thumb {
  position: relative;
  width: 100%;
  cursor: pointer;
}
.thumb img {
  width: 100%;
  height: auto;
  max-height: 240px;
  object-fit: contain;
  border-radius: 10px;
  border: 1px solid var(--border);
  display: block;
  background: #f8fafc;
}
.thumb-label {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  background: rgba(0, 0, 0, 0.6);
  padding: 3px 11px;
  border-radius: 999px;
}
.thumb-label.uploaded {
  background: var(--success);
}
.actions {
  display: flex;
  gap: 10px;
}
.tap-btn {
  flex: 1;
  flex-direction: column;
  gap: 6px;
  padding: 14px 8px;
  font-size: 19px;
  border-radius: 12px;
}
</style>
