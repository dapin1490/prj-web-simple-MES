<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
  /**
   * 시간순(과거→최신) 포인트. label은 X축 문자열.
   * cr_temp·temp_sp는 백엔드 미전달 시 null 가능(차트에서 구간 끊김).
   * @type {{ label: string, cr_temp: number | null, temp_sp: number | null, temp_pv: number, speed: number }[]}
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
  const crTempValues = props.points.map((point) =>
    point.cr_temp === null || point.cr_temp === undefined ? null : point.cr_temp,
  )
  const tempSpValues = props.points.map((point) =>
    point.temp_sp === null || point.temp_sp === undefined ? null : point.temp_sp,
  )
  const tempPvValues = props.points.map((point) => point.temp_pv)
  const speedValues = props.points.map((point) => point.speed)

  if (chartInstance) {
    chartInstance.data.labels = labels
    chartInstance.data.datasets[0].data = crTempValues
    chartInstance.data.datasets[1].data = tempSpValues
    chartInstance.data.datasets[2].data = tempPvValues
    chartInstance.data.datasets[3].data = speedValues
    chartInstance.update('none')
    return
  }

  chartInstance = new Chart(canvasRef.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'cr_temp 목표 (℃)',
          data: crTempValues,
          yAxisID: 'y',
          borderColor: '#2e7d32',
          backgroundColor: 'rgba(46, 125, 50, 0.08)',
          borderDash: [6, 4],
          fill: false,
          tension: 0.2,
          pointRadius: 0,
          spanGaps: false,
        },
        {
          label: 'temp_sp 지시 (℃)',
          data: tempSpValues,
          yAxisID: 'y',
          borderColor: '#f9a825',
          backgroundColor: 'rgba(249, 168, 37, 0.1)',
          fill: false,
          tension: 0.2,
          pointRadius: 0,
          spanGaps: false,
        },
        {
          label: 'temp_pv 실측 (℃)',
          data: tempPvValues,
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
  height: 320px;
}
</style>
