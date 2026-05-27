export async function login(payload) {
  const res = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return res.json();
}

export async function fetchProfile(username) {
  const res = await fetch(`/api/v1/auth/profile?username=${encodeURIComponent(username)}`);
  return res.json();
}
