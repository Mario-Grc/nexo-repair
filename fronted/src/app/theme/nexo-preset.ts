import { definePreset, palette } from '@primeng/themes';
import Aura from '@primeng/themes/aura';

export const NexoPreset = definePreset(Aura, {
  semantic: {
    primary: palette('#1E3A8A') as Record<string, string>
  }
});