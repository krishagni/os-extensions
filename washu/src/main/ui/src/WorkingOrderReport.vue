
<template>
  <os-button left-icon="download" label="Working Order Report"
    @click="downloadReport" v-show="showDownloadRpt" />
</template>

<script>
export default {
  props: ['order'],

  data() {
    return {
      showDownloadRpt: false
    };
  },

  created() {
    if (this.order.distributionProtocol.report && this.order.distributionProtocol.report.id1) {
      this.showDownloadRpt = true;
    } else {
      this.$osSvc.settingsSvc.getSetting('common', 'distribution_report_query').then(
        (resp) => {
          this.showDownloadRpt = resp && resp.length > 0 && !!resp[0].value;
        }
      );
    }
  },

  methods: {
    downloadReport: function() {
      const downloadFn = () => {
        const params = {orderId: this.order.id};
        return this.$osSvc.http.get('washu-reports/order-report', params);
      }
          
      this.$osSvc.util.downloadReport(downloadFn, {filename: this.order.name + '.xlsx'});
    }
  }
}
</script>
