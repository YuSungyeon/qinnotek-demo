<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { fileUrl } from '../api/client'
import Icon from './Icon.vue'

const props = defineProps({
  item: { type: Object, required: true }
})
const emit = defineEmits(['select'])

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
  <div class="photo-card" :class="{ returned: item.status === 'RETURNED' }" :data-item-id="item.itemId">
    <div class="head">
      <span class="name">{{ item.name }}</span>
      <span v-if="previewUrl" class="done-chip">✓ 선택됨</span>
      <span v-else class="todo-chip">미선택</span>
    </div>

    <p v-if="item.description" class="desc">{{ item.description }}</p>

    <div v-if="item.status === 'RETURNED' && item.rejectReason" class="reject">
      <strong>반환 사유</strong>
      <span>{{ item.rejectReason }}</span>
    </div>

    <!-- 예시 사진(위) → 내 사진(아래) 세로 배치 -->
    <div class="images">
      <div v-if="item.exampleImageUrl" class="thumb">
        <span class="thumb-label">예시</span>
        <img :src="fileUrl(item.exampleImageUrl)" alt="예시" />
      </div>
      <div v-if="previewUrl" class="thumb">
        <span class="thumb-label uploaded">내 사진</span>
        <img :src="previewUrl" alt="선택한 사진" />
      </div>
    </div>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 10px">{{ error }}</div>

    <div class="actions">
      <button class="btn btn-ghost btn-sm" @click="pickCamera">
        <Icon name="camera" :size="18" /> 카메라 촬영
      </button>
      <button class="btn btn-ghost btn-sm" @click="pickGallery">
        <Icon name="image" :size="18" /> 사진첩
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
}
.photo-card.returned {
  border-color: #fca5a5;
  background: #fffafa;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.name {
  font-size: 24px;
  font-weight: 700;
}
.done-chip {
  font-size: 18px;
  font-weight: 700;
  color: var(--success);
}
.todo-chip {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-muted);
}
.desc {
  margin: 0 0 10px;
  font-size: 21px;
  color: var(--text-muted);
}
.reject {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--danger-bg);
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  font-size: 21px;
}
.reject strong {
  color: var(--danger);
  font-size: 18px;
}
/* 예시(위) → 내 사진(아래) 세로 배치, 각 이미지는 카드 가로 폭에 맞춤 */
.images {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}
.thumb {
  position: relative;
  width: 100%;
}
.thumb img {
  width: 100%;
  height: auto;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid var(--border);
  display: block;
}
.thumb-label {
  position: absolute;
  top: 6px;
  left: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  padding: 2px 9px;
  border-radius: 999px;
}
.thumb-label.uploaded {
  background: var(--success);
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.actions .btn-sm {
  font-size: 20px;
  padding: 10px 16px;
}
</style>
