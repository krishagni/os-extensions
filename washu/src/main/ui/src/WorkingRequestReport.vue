
<template>
  <os-button left-icon="download" label="Working Request Report"
    @click="downloadReport" v-show="showDownloadRpt" />
</template>

<script>
export default {
  props: ['request', 'selected-specimens'],

  data() {
    return {
      showDownloadRpt: false
    };
  },

  created() {
    this.$osSvc.settingsSvc.getSetting('common', 'cart_specimens_rpt_query').then(
      (resp) => {
        this.showDownloadRpt = resp && resp.length > 0 && !!resp[0].value;
      }
    );
  },

  methods: {
    downloadReport: function() {
      const downloadFn = () => {
        const params = {requestId: this.request.id};
        if (this.selectedSpecimens && this.selectedSpecimens.length > 0) {
          params.specimenId = this.selectedSpecimens.map(spmn => spmn.id);
        }

        return this.$osSvc.http.get('washu-reports/request-report', params);
      }
          
      this.$osSvc.util.downloadReport(downloadFn, {filename: 'request_' + this.request.id + '.xlsx'});
    }
  }
}
</script>
