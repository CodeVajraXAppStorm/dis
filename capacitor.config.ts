import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.vibe.jarvis',
  appName: 'Jarvis',
  webDir: 'www',
  plugins: {
    CapacitorHttp: { enabled: true }
  }
};

export default config;
