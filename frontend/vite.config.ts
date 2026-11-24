import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const configuration = loadEnv(mode, '..', 'POOL_');
  const target = 'http://localhost:' + (configuration.POOL_APP_PORT || '8080');
  return {
    server: {
      proxy: {
        '/api': target,
        '/swagger-ui': target,
        '/v3/api-docs': target,
      },
    },
  };
});
