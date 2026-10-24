import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.family.rentmanager',
  appName: 'Rent Manager',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  }
};

export default config;
