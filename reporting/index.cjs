const express = require('express')
const fs = require("fs");
const cors = require('cors')
const app = express()
const port = 8080

app.use(cors());

const simulationDir = '../simulations';
app.get('/experiments', (req, res) => {
  const apps = fs.readdirSync(simulationDir)
  const experiments = apps.map(dir => ({
    app: dir,
    experiments: fs.readdirSync(`${simulationDir}/${dir}`)
  }))
  res.send(experiments)
})

app.get('/experiments/:app/:experiment', (req, res) => {
  const folder = `${simulationDir}/${req.params.app}/${req.params.experiment}`
  const setup = fs.readFileSync(`${folder}/setup.json`)
  res.json({
    setup: JSON.parse(setup)
  })
})

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`)
})