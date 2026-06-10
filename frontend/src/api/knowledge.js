const pendingKnowledgeApi = async (fallback) => fallback;

export async function listKnowledgeBases() {
  return pendingKnowledgeApi([]);
}

export async function createKnowledgeBase() {
  return pendingKnowledgeApi(null);
}

export async function listDocuments() {
  return pendingKnowledgeApi([]);
}

export async function createDocument() {
  return pendingKnowledgeApi(null);
}
