import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import mkcert from 'vite-plugin-mkcert';
import fs from 'node:fs';
import { IncomingMessage, ServerResponse } from "http";

const simulationDir = '../simulations'

function sendJson(res: ServerResponse<IncomingMessage>, obj: any): string {
  const result = JSON.stringify(obj)
  res.writeHead(200, {
    'Content-Type': 'application/json',
  })
  res.write(result)
  res.end()
  return result
}

export default defineConfig({
  server: {
    https: true,
    proxy: {
      '/experiments': {
        bypass(_, res) {
          const apps = fs.readdirSync(simulationDir)
          const experiments = apps.map(dir => ({
            app: dir,
            experiments: fs.readdirSync(`${simulationDir}/${dir}`)
          }))
          return sendJson(res, experiments)
        }
      },
      '/experiment': {
        bypass(req, res) {
          const request = new URL(`http://localhost${req.url}`)
          const app = request.searchParams.get('app')
          const experiment = request.searchParams.get('experiment')

          const folder = `${simulationDir}/${app}/${experiment}`
          const setup = JSON.parse(fs.readFileSync(`${folder}/setup.json`).toString())

          return sendJson(res, { setup })
        }
      }
    }
  },
  plugins: [ vue(), mkcert() ]
})
