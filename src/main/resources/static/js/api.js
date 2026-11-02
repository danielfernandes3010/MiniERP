const api = {
  async request(path, options = {}) {
    const response = await fetch(path, {
      ...options,
      headers: { "Content-Type": "application/json", ...(options.headers || {}) }
    });
    const payload = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
      const fieldMessages = payload?.fieldErrors ? Object.values(payload.fieldErrors).join(" ") : "";
      throw new Error(fieldMessages || payload?.message || `Falha na requisição (${response.status}).`);
    }
    return payload;
  },
  get(path) { return this.request(path); },
  post(path, body) { return this.request(path, { method: "POST", body: JSON.stringify(body) }); },
  put(path, body) { return this.request(path, { method: "PUT", body: JSON.stringify(body) }); },
  patch(path) { return this.request(path, { method: "PATCH" }); }
};

export default api;
