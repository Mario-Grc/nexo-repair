import '@angular/compiler';
import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { requireEmailOrPhone } from './customer-form-dialog';

function group(email: string | null, phone: string | null) {
  return new FormGroup({
    email: new FormControl(email),
    phone: new FormControl(phone),
  });
}

describe('requireEmailOrPhone', () => {
  it('accepts only email', () => {
    expect(requireEmailOrPhone(group('ana@nexo.com', ''))).toBeNull();
  });

  it('accepts only phone', () => {
    expect(requireEmailOrPhone(group('', '600123123'))).toBeNull();
  });

  it('accepts both', () => {
    expect(requireEmailOrPhone(group('ana@nexo.com', '600123123'))).toBeNull();
  });

  it('rejects both empty', () => {
    expect(requireEmailOrPhone(group('', ''))).toEqual({ contactRequired: true });
  });

  it('rejects both null', () => {
    expect(requireEmailOrPhone(group(null, null))).toEqual({ contactRequired: true });
  });

  it('rejects whitespace only', () => {
    expect(requireEmailOrPhone(group('   ', '  '))).toEqual({ contactRequired: true });
  });
});
