// Backend API endpoint
const API_URL = "/state";

// Select UI elements
const systemStateEl = document.getElementById("system_state");
const avgTempEl = document.getElementById("avgTemp");
const maxTempEl = document.getElementById("maxTemp");
const minTempEl = document.getElementById("minTemp");
const windowAngleEl = document.getElementById("angle");
const windowStateCheckBEl = document.getElementById("window_state");
const windowStateTextEl = document.getElementById("window_state_text");
window_state = "automatic";

// Temperature Graph Setup
let temperatureChart;
const ctx = document.getElementById("temperatureGraph").getContext("2d");

// Initialize an empty Chart
function initChart() {
    temperatureChart = new Chart(ctx, {
        type: "line",
        data: {
            labels: [],
            datasets: [{
                label: "Temperature (°C)",
                data: [],
                borderColor: "blue",
                backgroundColor: "rgba(0, 0, 255, 0.1)",
                fill: true
            }]
        },
        options: {
            responsive: false, // Disable auto-resizing
            maintainAspectRatio: false, // Prevent forced aspect ratio
            scales: {
                x: { title: { display: true, text: "Time" } },
                y: { title: { display: true, text: "Temperature (°C)" } }
            }
        }
    });
}


// Fetch updated data from the backend
async function fetchSystemState() {
    try {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error("Failed to fetch system state");

        const data = await response.json();

        // Update UI with received data
        systemStateEl.textContent = data.system_state;
        avgTempEl.textContent = data.avgTemp;
        maxTempEl.textContent = data.maxTemp;
        minTempEl.textContent = data.minTemp;
        windowAngleEl.value = data.angle;
        if (data.window_state == "manual" || data.window_state == "automatic") {
            window_state = data.window_state;
            changeWindowState();
        }

        // Update chart if data exists
        if (data.graph) {
            updateChart(data.graph);
        }

    } catch (error) {
        console.error("Error fetching system state:", error);
    }
}

function changeWindowState() {
    if (window_state == "automatic") {
        windowStateCheckBEl.checked = true;
        windowStateTextEl.innerText = "Window State: Automatic ";
        windowAngleEl.disabled = true;
    } else if (window_state == "manual") {
        windowStateCheckBEl.checked = false;
        windowStateTextEl.innerText = "Window State: Manual ";
        windowAngleEl.disabled = false;
    }
}

// Update chart data dynamically
function updateChart(newData) {
    if (!temperatureChart) return;

    temperatureChart.data.labels = newData.labels; // Time labels
    temperatureChart.data.datasets[0].data = newData.temperatures; // Temperature values
    temperatureChart.update();
}

windowAngleEl.addEventListener("onchange", () => {
    if (window_state == "manual") {

    }
});

// Initialize the chart and start fetching data
initChart();
fetchSystemState(); // Fetch initial state
setInterval(fetchSystemState, 5000); // Poll the backend every 5 seconds
