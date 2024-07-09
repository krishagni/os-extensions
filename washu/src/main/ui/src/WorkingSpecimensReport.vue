
<template>
  <os-button left-icon="download" label="Working Specimens Report"
    @click="downloadReport" v-show="showDownloadRpt" />
</template>

<script>
export default {
  props: ['cart', 'selected-specimens'],

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
        const params = {listId: this.cart.id};
        console.log(this.selectedSpecimens);
        if (this.selectedSpecimens && this.selectedSpecimens.length > 0) {
          params.specimenId = this.selectedSpecimens.map(spmn => spmn.id);
        }

        return this.$osSvc.http.get('washu-reports/working-specimens', params);
      }
          
      this.$osSvc.util.downloadReport(downloadFn, {filename: this.cart.name + '.xlsx'});
    }
  }
}
</script>
