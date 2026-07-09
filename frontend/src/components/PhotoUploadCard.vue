<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { fileUrl } from '../api/client'
import Icon from './Icon.vue'

const props = defineProps({
  item: { type: Object, required: true },
  index: { type: Number, default: 0 },
  /** 선택된 파일 (부모 관리 - 직접 선택/자동 분류 모두 이 prop으로 반영) */
  file: { type: File, default: null }
})
const emit = defineEmits(['select', 'zoom'])

const cameraInput = ref(null)
const galleryInput = ref(null)
const previewUrl = ref('')
const error = ref('')

// file prop → 미리보기 URL 동기화
watch(
  () => props.file,
  (f) => {
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = f ? URL.createObjectURL(f) : ''
  },
  { immediate: true }
)

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
        <template v-else>{{ index || '' }}</template>
      </span>
      <span class="name">{{ item.name }}</span>
      <span class="status-chip" :class="previewUrl ? 'ok' : 'todo'">
        {{ previewUrl ? '완료' : '필요' }}
      </span>
    </div>

    <p v-if="item.description" class="desc">{{ item.description }}</p>

    <div v-if="item.adminNote" class="admin-note">
      <strong>요청 사항</strong>
      <span>{{ item.adminNote }}</span>
    </div>

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
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex: 0 0 auto;
  font-size: 18px;
  font-weight: 800;
  color: var(--primary);
  background: var(--primary-soft);
}
.photo-card.done .step {
  color: #fff;
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
.admin-note {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--primary-soft);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
  font-size: 21px;
  line-height: 1.5;
}
.admin-note strong {
  color: var(--primary-dark);
  font-size: 17px;
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
  aspect-ratio: 1 / 1; /* 가로·세로 사진 모두 동일한 정사각형 박스에 표시 */
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #f1f5f9;
  overflow: hidden;
  cursor: pointer;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 잘리지 않고 전체가 보이도록 */
  display: block;
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
