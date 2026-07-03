<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '../composables/useAuth';
import { deleteDocument, fetchDocuments } from '../service/documentApi';
import type { Document as DocumentItem } from '../types/document';

const props = withDefaults(
    defineProps<{
        itemsPerPage?: number;
        title?: string;
        subtitle?: string;
    }>(),
    {
        itemsPerPage: 8,
        title: 'Document Library',
        subtitle: 'Browse confidential records.',
    },
);

const documents = ref<DocumentItem[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const page = ref(1);
const deletingDocumentId = ref<number | null>(null);
const router = useRouter();
const { logout } = useAuth();

const loadDocuments = async () => {
    loading.value = true;
    error.value = null;

    try {
        documents.value = await fetchDocuments();
    } catch (loadError) {
        error.value = loadError instanceof Error ? loadError.message : 'Failed to load documents';
    } finally {
        loading.value = false;
    }
};

const openSecureEditor = async () => {
    await router.push('/secure-editor');
};

const openDocument = async (documentId: number) => {
    await router.push({ path: '/secure-editor', query: { documentId: String(documentId) } });
};

const handleLogout = async () => {
    logout();
    await router.push('/login');
};

const handleDeleteDocument = async (documentId: number) => {
    const shouldDelete = window.confirm('Delete this document permanently?');
    if (!shouldDelete) {
        return;
    }

    deletingDocumentId.value = documentId;

    try {
        await deleteDocument(documentId);
        await loadDocuments();
    } catch (deleteError) {
        error.value = deleteError instanceof Error ? deleteError.message : 'Failed to delete document';
    } finally {
        deletingDocumentId.value = null;
    }
};

const totalPages = computed(() => {
    if (!documents.value.length) {
        return 1;
    }
    return Math.ceil(documents.value.length / props.itemsPerPage);
});

const paginatedDocuments = computed(() => {
    const start = (page.value - 1) * props.itemsPerPage;
    return documents.value.slice(start, start + props.itemsPerPage);
});

onMounted(loadDocuments);

watch(
    () => documents.value.length,
    () => {
        page.value = Math.min(page.value, totalPages.value);
    },
);

const formatDate = (raw?: string | null): string => {
  if (!raw) return "";
  const d = new Date(raw);
  if (isNaN(d.getTime())) return String(raw);
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

const previewContent = (content: string) => {
    const compact = content.trim().replace(/\s+/g, ' ');
    return compact.length > 180 ? `${compact.slice(0, 180)}...` : compact;
};
</script>

<template>
    <v-container fluid class="pa-4 pa-sm-6">
        
        <div class="d-flex flex-column flex-sm-row justify-space-between align-start align-sm-center mb-8" style="gap: 16px;">
            
            <div class="d-flex flex-column align-start text-left">
                <div class="text-overline text-primary mb-1">Secure Archive</div>
                <h2 class="text-h4 font-weight-bold mb-2">{{ title }}</h2>
                <p class="text-subtitle-1 text-medium-emphasis mb-0">{{ subtitle }}</p>
            </div>

            <div class="d-flex align-center flex-wrap" style="gap: 12px;">
                <v-chip color="primary" variant="tonal" label size="large">
                    {{ documents.length }} documents
                </v-chip>

                <v-btn
                    color="primary"
                    variant="flat"
                    prepend-icon="mdi-plus"
                    size="large"
                    @click="openSecureEditor"
                >
                    Create document
                </v-btn>

                <v-btn
                    color="secondary"
                    variant="tonal"
                    prepend-icon="mdi-logout"
                    size="large"
                    @click="handleLogout"
                >
                    Logout
                </v-btn>
            </div>
        </div>

        <v-card v-if="loading" variant="tonal" rounded="lg" class="mb-4 border-dashed">
            <v-card-text class="text-center py-12">
                <v-progress-circular indeterminate color="primary" size="48" class="mb-4" />
                <h3 class="text-h6 font-weight-bold mb-2">Loading documents</h3>
                <p class="text-medium-emphasis mb-0">Fetching the secure archive from the API.</p>
            </v-card-text>
        </v-card>

        <v-card v-else-if="error" variant="tonal" rounded="lg" color="error" class="mb-4 border-dashed">
            <v-card-text class="text-center py-12">
                <v-icon icon="mdi-alert-circle-outline" size="48" class="mb-4" />
                <h3 class="text-h6 font-weight-bold mb-2">Could not load documents</h3>
                <p class="mb-6">{{ error }}</p>
                <v-btn color="error" variant="flat" @click="loadDocuments">Retry</v-btn>
            </v-card-text>
        </v-card>

        <v-row v-else-if="paginatedDocuments.length" dense>
            <v-col
                v-for="document in paginatedDocuments"
                :key="document.id"
                cols="12"
                sm="6"
                md="4"
                lg="3"
            >
                <v-card
                    class="h-100 d-flex flex-column document-card-clickable"
                    elevation="2"
                    rounded="lg"
                    role="button"
                    tabindex="0"
                    @click="openDocument(document.id)"
                    @keydown.enter.prevent="openDocument(document.id)"
                    @keydown.space.prevent="openDocument(document.id)"
                >
                    <v-card-text class="d-flex flex-column flex-grow-1">
                        
                        <div class="d-flex align-center justify-space-between mb-4">
                            <v-chip size="small" color="primary" variant="tonal" label>
                                ID {{ document.id }}
                            </v-chip>

                            <div class="d-flex align-center" style="gap: 8px;">
                                <span class="text-caption text-medium-emphasis">{{ formatDate(document.lastModified) }}</span>
                                <v-btn
                                    icon="mdi-delete-outline"
                                    size="small"
                                    color="error"
                                    variant="text"
                                    :loading="deletingDocumentId === document.id"
                                    :disabled="deletingDocumentId !== null"
                                    :aria-label="`Delete document ${document.id}`"
                                    @click.stop="handleDeleteDocument(document.id)"
                                />
                            </div>
                        </div>

                        <h3 class="text-subtitle-1 font-weight-bold text-high-emphasis mb-2">
                            Document {{ document.id }}
                        </h3>

                        <p class="text-body-2 text-medium-emphasis mb-0 document-content-clamp">
                            {{ previewContent(document.content) }}
                        </p>

                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <v-card v-else variant="tonal" rounded="lg" class="mb-4 border-dashed">
            <v-card-text class="text-center py-12">
                <v-icon icon="mdi-file-document-outline" size="48" color="primary" class="mb-4" />
                <h3 class="text-h6 font-weight-bold mb-2">No documents available</h3>
                <p class="text-medium-emphasis mb-0">Add documents to populate the secure archive.</p>
            </v-card-text>
        </v-card>

        <div v-if="documents.length > itemsPerPage" class="d-flex justify-center mt-8">
            <v-pagination v-model="page" :length="totalPages" rounded="circle" active-color="primary" />
        </div>

    </v-container>
</template>

<style scoped>
.document-content-clamp {
    line-clamp: 5;
    display: -webkit-box;
    -webkit-line-clamp: 5;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.border-dashed {
    border: 2px dashed rgba(var(--v-theme-on-surface), 0.16) !important;
}

.document-card-clickable {
    cursor: pointer;
    transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.document-card-clickable:hover {
    transform: translateY(-2px);
}
</style>
