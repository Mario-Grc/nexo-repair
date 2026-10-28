import { definePreset, palette } from '@primeng/themes';
import Aura from '@primeng/themes/aura';

export const NexoPreset = definePreset(Aura, {
  semantic: {
    primary: palette('#1E3A8A') as Record<string, string>,
    colorScheme: {
      light: {
        surface: {
          0: '#FBFAF6',
          50: '#F3F1EA',
          100: '#ECE9DF',
          200: '#D9D5C8',
          300: '#CFCABB',
          400: '#A7A395',
          500: '#7A7769',
          600: '#454E60',
          700: '#2E3648',
          800: '#232A3A',
          900: '#1B2233',
          950: '#121825',
        },
        text: { color: '{surface.900}', mutedColor: '#5A6272' },
      },
    },
  },
});