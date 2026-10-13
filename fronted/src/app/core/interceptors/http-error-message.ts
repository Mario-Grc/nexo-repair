// Pure mapping from HTTP failures to user-facing messages. Kept free of
// Angular imports so Vitest can load it without the Angular compiler.
export function errorMessage(status: number, serverMessage?: string): string {
  if (status === 0) return 'No connection to the server.';
  if (status === 400 || status === 409) return serverMessage?.trim() ? serverMessage : 'The data is not valid.';
  if (status === 403) return 'You do not have permission for this action.';
  if (status === 404) return 'Not found.';
  if (status >= 500 && status < 600) return 'Server error. Please try again.';
  return 'Something went wrong. Please try again.';
}

export function readServerMessage(body: unknown): string | undefined {
  if (typeof body === 'string') return body.trim() ? body : undefined;
  if (Array.isArray(body)) {
    const text = body.filter(part => typeof part === 'string').join('; ');
    return text.trim() ? text : undefined;
  }
  if (typeof body === 'object' && body !== null && 'message' in body) {
    const message = (body as { message: unknown }).message;
    if (typeof message === 'string' && message.trim()) return message;
    if (Array.isArray(message)) return readServerMessage(message);
  }
  return undefined;
}
