import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * Frontend modules live under /api/*.js (folder name `api/`), which collides with
 * backend proxy `/api/*`. Bypass static module requests so Vite serves them.
 */
function isFrontendModule(url = '') {
  return /\.(?:[cm]?js|mjs|ts|tsx|vue|css|scss|sass|less|map|json)(?:\?|$)/i.test(url)
}

export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      '/__backend_api__': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        bypass(req) {
          if (isFrontendModule(req.url || '')) {
            return req.url
          }
        }
      }
    }
  }
})
