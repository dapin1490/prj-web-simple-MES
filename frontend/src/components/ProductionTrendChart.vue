<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
  /**
   * 시간순(과거→최신) 포인트. label은 X축 문자열.
   * @type {{ label: string, temp_pv: number, speed: number }[]}
   */
  points: {
    type: Array,
    default: () => [],
  },
})

const canvasRef = ref(null)
let chartInstance = null

function applyChartData() {
  if (!canvasRef.value) {
    return
  }

  const labels = props.points.map((point) => point.label)
  const temperatureValues = props.points.map((point) => point.temp_pv)
  const speedValues = props.points.map((point) => point.speed)

  if (chartInstance) {
    chartInstance.data.labels = labels
    chartInstance.data.datasets[0].data = temperatureValues
    chartInstance.data.datasets[1].data = speedValues
    chartInstance.update('none')
    return
  }

  chartInstance = new Chart(canvasRef.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'temp_pv (℃)',
          data: temperatureValues,
          yAxisID: 'y',
          borderColor: '#c62828',
          backgroundColor: 'rgba(198, 40, 40, 0.12)',
          fill: false,
          tension: 0.2,
          pointRadius: 0,
        },
        {
          label: 'speed (m/min)',
          data: speedValues,
          yAxisID: 'y1',
          borderColor: '#1565c0',
          backgroundColor: 'rgba(21, 101, 192, 0.12)',
          fill: false,
          tension: 0.2,
          pointRadius: 0,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false,
      },
      scales: {
        x: {
          title: {
            display: true,
            text: '시간',
          },
        },
        y: {
          type: 'linear',
          position: 'left',
          title: {
            display: true,
            text: '℃',
          },
        },
        y1: {
          type: 'linear',
          position: 'right',
          grid: {
            drawOnChartArea: false,
          },
          title: {
            display: true,
            text: 'm/min',
          },
        },
      },
    },
  })
}

onMounted(() => {
  applyChartData()
})

watch(
  () => props.points,
  () => {
    applyChartData()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.destroy()
    chartInstance = null
  }
})
</script>

<template>
  <div class="production-trend-chart">
    <canvas ref="canvasRef" />
  </div>
</template>

<style scoped>
.production-trend-chart {
  position: relative;
  height: 280px;
}
</style>
