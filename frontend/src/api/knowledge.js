export async function listKnowledgeBases() {
  const res = await fetch('/api/v1/knowledge-bases');
  return res.json();
}

export async function createKnowledgeBase(payload) {
  const res = await fetch('/api/v1/knowledge-bases', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return res.json();
}

export async function listDocuments(knowledgeBaseId) {
  const res = await fetch(`/api/v1/documents?knowledgeBaseId=${knowledgeBaseId}`);
  return res.json();
}

export async function createDocument(payload) {
  const res = await fetch('/api/v1/documents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return res.json();
}
