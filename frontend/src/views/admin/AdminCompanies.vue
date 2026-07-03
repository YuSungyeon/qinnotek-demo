<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api/admin'

const router = useRouter()
const companies = ref([])
const keyword = ref('')
const loading = ref(false)
const error = ref('')

const showCreate = ref(false)
const newName = ref('')
const creating = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    companies.value = await adminApi.searchCompanies(keyword.value)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function createCompany() {
  if (!newName.value.trim()) return
  creating.value = true
  try {
    const created = await adminApi.createCompany(newName.value.trim())
    newName.value = ''
    showCreate.value = false
    router.push(`/admin/companies/${created.id}`)
  } catch (err) {
    error.value = err.message
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <h1>기업 관리</h1>
      <button class="btn" @click="showCreate = !showCreate">+ 기업 등록</button>
    </div>

    <div v-if="showCreate" class="card create-box">
      <label class="label">기업명</label>
      <div class="row">
        <input v-model="newName" class="input" placeholder="기업명 입력" @keyup.enter="createCompany" />
        <button class="btn" :disabled="creating" @click="createCompany">등록</button>
      </div>
    </div>

    <div class="search-row">
      <input
        v-model="keyword"
        class="input"
        placeholder="기업명 또는 전화번호 검색"
        @keyup.enter="load"
      />
      <button class="btn btn-ghost" @click="load">검색</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="loading" class="muted">불러오는 중…</div>

    <table v-else class="tbl">
      <thead>
        <tr>
          <th>기업명</th>
          <th>전화번호</th>
          <th>상태</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in companies" :key="c.id" class="clickable" @click="router.push(`/admin/companies/${c.id}`)">
          <td class="strong">{{ c.name }}</td>
          <td>{{ c.phoneNumber || '—' }}</td>
          <td>
            <span class="badge" :style="{ background: c.statusColor }">{{ c.statusLabel }}</span>
          </td>
        </tr>
        <tr v-if="companies.length === 0">
          <td colspan="3" class="muted center">등록된 기업이 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-head h1 {
  font-size: 25px;
  font-weight: 800;
}
.create-box {
  margin-bottom: 16px;
}
.row {
  display: flex;
  gap: 8px;
}
.row > .input {
  flex: 1;
  min-width: 0;
}
.search-row {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
}
.search-row > .input {
  flex: 1;
  min-width: 0;
}
.tbl {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: var(--card);
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}
.tbl th,
.tbl td {
  text-align: left;
  padding: 15px 18px;
  border-bottom: 1px solid var(--border);
  font-size: 15px;
}
.tbl th {
  background: #f8fafc;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.tbl tbody tr {
  transition: background 0.12s;
}
.tbl tbody tr:last-child td {
  border-bottom: none;
}
.clickable {
  cursor: pointer;
}
.clickable:hover {
  background: var(--primary-soft);
}
.strong {
  font-weight: 700;
}
.center {
  text-align: center;
}
</style>
