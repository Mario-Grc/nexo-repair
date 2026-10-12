import { describe, expect, it } from 'vitest';
import { errorMessage } from './http-error.interceptor';

describe('errorMessage', () => {
  it('reports no connection on status 0', () => {
    expect(errorMessage(0)).toBe('No connection to the server.');
  });

  it('prefers the server message on 400', () => {
    expect(errorMessage(400, 'A note is required.')).toBe('A note is required.');
  });

  it('falls back on 400 without a server message', () => {
    expect(errorMessage(400)).toBe('The data is not valid.');
    expect(errorMessage(400, '')).toBe('The data is not valid.');
  });

  it('prefers the server message on 409', () => {
    expect(errorMessage(409, 'The ticket is closed.')).toBe('The ticket is closed.');
  });

  it('falls back on 409 without a server message', () => {
    expect(errorMessage(409)).toBe('The data is not valid.');
  });

  it('reports forbidden on 403', () => {
    expect(errorMessage(403)).toBe('You do not have permission for this action.');
  });

  it('reports not found on 404', () => {
    expect(errorMessage(404)).toBe('Not found.');
  });

  it('reports a server error on 500', () => {
    expect(errorMessage(500)).toBe('Server error. Please try again.');
  });

  it('falls back on an unmapped status', () => {
    expect(errorMessage(418)).toBe('Something went wrong. Please try again.');
  });
});
