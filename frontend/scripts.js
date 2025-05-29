const socket = io('http://localhost:3000'); // WebSocket connection to the backend
let tagHistory = []; // Store tag history

// Function to handle tab switching
function showTab(tabName) {
  const tabs = document.querySelectorAll('.tab');
  tabs.forEach(tab => {
    tab.style.display = tab.id === tabName ? 'block' : 'none';
  });
}

// Fetch the tags from the backend API when the page loads
window.onload = function () {
  fetchTags();
};

// Function to fetch tags from backend and display them in the live view
async function fetchTags() {
  try {
    const response = await fetch('http://localhost:8080/api/tags');
    if (!response.ok) {
      throw new Error('Failed to fetch tags');
    }
    const tags = await response.json();
    displayTags(tags);
  } catch (error) {
    console.error('Error fetching tags:', error);
  }
}

// Function to display tags in the live view section
function displayTags(tags) {
  const liveTagList = document.getElementById('liveTagList');
  liveTagList.innerHTML = ''; // Clear the current list

  tags.forEach(tag => {
    const listItem = document.createElement('li');
    listItem.innerText = `EPC: ${tag.epc}, Timestamp: ${tag.timestamp}`;
    liveTagList.appendChild(listItem);
  });
}

// Handle live tag scanning
socket.on('tagScanned', (tag) => {
  console.log('Tag Scanned:', tag);

  // Add the new tag to the live view list
  const liveTagList = document.getElementById('liveTagList');
  const listItem = document.createElement('li');
  listItem.innerText = `EPC: ${tag.epc}, Timestamp: ${tag.timestamp}`;
  liveTagList.appendChild(listItem);

  // Add to tag history
  tagHistory.push(tag);
  updateHistoryTable();
});

// Update the history table
function updateHistoryTable() {
  const historyTableBody = document.querySelector('#historyTable tbody');
  historyTableBody.innerHTML = ''; // Clear the table

  tagHistory.forEach(tag => {
    const row = document.createElement('tr');
    row.innerHTML = `<td>${tag.epc}</td><td>${tag.rssi}</td><td>${tag.timestamp}</td>`;
    historyTableBody.appendChild(row);
  });
}

// Filter history based on EPC
function filterHistory() {
  const searchQuery = document.getElementById('searchBox').value.toLowerCase();
  const filteredHistory = tagHistory.filter(tag =>
    tag.epc.toLowerCase().includes(searchQuery)
  );
  
  // Update the history table with the filtered data
  const historyTableBody = document.querySelector('#historyTable tbody');
  historyTableBody.innerHTML = ''; // Clear the table

  filteredHistory.forEach(tag => {
    const row = document.createElement('tr');
    row.innerHTML = `<td>${tag.epc}</td><td>${tag.rssi}</td><td>${tag.timestamp}</td>`;
    historyTableBody.appendChild(row);
  });
}

// Export tag history to CSV
function exportCSV() {
  const csvRows = [];
  const headers = ['EPC', 'RSSI', 'Timestamp'];
  csvRows.push(headers.join(','));

  tagHistory.forEach(tag => {
    const row = [tag.epc, tag.rssi, tag.timestamp].join(',');
    csvRows.push(row);
  });

  // Create a CSV file and trigger download
  const csvContent = csvRows.join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = 'tag_history.csv';
  link.click();
}

function printReceipt() {
  fetch('http://localhost:8080/api/tags')
    .then(res => res.json())
    .then(tags => {
      const tagMap = new Map();
      let grandTotal = 0;

      tags.forEach(tag => {
        const epc = tag.epc;
        const product = tag.product || { name: "Unknown", price: 0.0 };
        if (!tagMap.has(epc)) {
          tagMap.set(epc, {
            epc: epc,
            name: product.name,
            price: product.price,
            quantity: 1
          });
        } else {
          tagMap.get(epc).quantity += 1;
        }
      });

      const finalTags = [];
      tagMap.forEach(entry => {
        const total = entry.price * entry.quantity;
        grandTotal += total;
        for (let i = 0; i < entry.quantity; i++) {
          finalTags.push({
            epc: entry.epc,
            product: { name: entry.name, price: entry.price }
          });
        }
      });

      const receipt = {
        id: "R-" + Date.now(),
        timestamp: new Date().toISOString(),
        total: grandTotal,
        items: finalTags
      };

      // POST the receipt to backend
      fetch('http://localhost:8080/api/receipts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(receipt)
      }).then(() => {
        // Show receipt in new window
        const printWindow = window.open('', '_blank');
        let html = `
          <html><head><title>Jiffy Receipt</title></head><body>
          <h2>🧾 Receipt #${receipt.id}</h2>
          <p>${new Date(receipt.timestamp).toLocaleString()}</p>
          <table border="1" cellpadding="6" cellspacing="0">
            <tr><th>EPC</th><th>Product</th><th>Price (OMR)</th></tr>`;

        receipt.items.forEach(item => {
          html += `<tr><td>${item.epc}</td><td>${item.product.name}</td><td>${item.product.price.toFixed(3)}</td></tr>`;
        });

        html += `
            <tr><td colspan="2"><strong>Total</strong></td><td><strong>${receipt.total.toFixed(3)} OMR</strong></td></tr>
          </table>
          <p>Thank you for shopping with Jiffy!</p>
          <script>window.onload = function() { window.print(); }</script>
          </body></html>`;

        printWindow.document.write(html);
        printWindow.document.close();

        // Clear the tags
        clearTags();
      });
    });
}

