
import WorkingOrderReport from './WorkingOrderReport.vue';
import WorkingRequestReport from './WorkingRequestReport.vue';
import WorkingSpecimensReport from './WorkingSpecimensReport.vue';

export default {
  install(app, { osSvc }) {
    const pluginsReg = osSvc.pluginViews;

    app.component('washuWorkingSpecimensReport', WorkingSpecimensReport);
    const specimensReport = {name: 'washu-working-specimens-report', component: 'washuWorkingSpecimensReport'};
    pluginsReg.registerView('cart-specimens', 'page-header', specimensReport);

    app.component('washuWorkingOrderReport', WorkingOrderReport);
    const orderReport = {name: 'washu-working-order-report', component: 'washuWorkingOrderReport'};
    pluginsReg.registerView('order-detail', 'more-menu', orderReport);

    app.component('washuWorkingRequestReport', WorkingRequestReport);
    const requestReport = {name: 'washu-working-request-report', component: 'washuWorkingRequestReport'};
    pluginsReg.registerView('tracker-request-specimens', 'page-header', requestReport);
  }
}
