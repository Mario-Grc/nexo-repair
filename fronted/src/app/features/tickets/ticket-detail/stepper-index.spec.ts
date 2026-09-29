import { describe, expect, it } from 'vitest';
import { getStepperIndex } from './stepper-index';

// Expected order is handwritten from the product rule: the stepper is linear
// with parts before work, and CANCELLED stays out of the flow.
describe('getStepperIndex', () => {
  it('maps PENDING to 0', () => {
    expect(getStepperIndex('PENDING')).toBe(0);
  });

  it('maps WAITING_FOR_PARTS to 1', () => {
    expect(getStepperIndex('WAITING_FOR_PARTS')).toBe(1);
  });

  it('maps IN_PROGRESS to 2', () => {
    expect(getStepperIndex('IN_PROGRESS')).toBe(2);
  });

  it('maps COMPLETED to 3', () => {
    expect(getStepperIndex('COMPLETED')).toBe(3);
  });

  it('maps DELIVERED to 4', () => {
    expect(getStepperIndex('DELIVERED')).toBe(4);
  });

  it('maps CANCELLED to -1 because it is outside the flow', () => {
    expect(getStepperIndex('CANCELLED')).toBe(-1);
  });
});
