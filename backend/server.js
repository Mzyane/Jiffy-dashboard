const express = require('express');
const app = express();
const port = 8080;

// Middleware to parse JSON bodies
app.use(express.json());

// In-memory array to store tag info
let tags = [];

// POST /api/tags — receives tag info (EPC, product, price, timestamp)
app.post('/api/tags', (req, res) => {
  const { epc, product, price, timestamp } = req.body;

  if (!epc || !product || price === undefined || !timestamp) {
    return res.status(400).json({ error: 'Missing required tag fields.' });
  }

  const tagId = tags.length + 1;
  const newTag = {
    tagId,
    epc,
    product,
    price,
    timestamp,
  };

  tags.push(newTag);
  console.log('Received tag:', newTag);
  res.status(201).json({ message: 'Tag received', tag: newTag });
});

// GET /api/tags — returns all tags as JSON
app.get('/api/tags', (req, res) => {
  res.json(tags);
});

// Start server
app.listen(port, () => {
  console.log(`Backend server running at http://localhost:${port}`);
});
