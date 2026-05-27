export async function askQuestion(question) {
  const response = await fetch('/api/v1/qa/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question })
  });

  if (!response.ok) {
    throw new Error('问答接口调用失败');
  }

  return response.json();
}
