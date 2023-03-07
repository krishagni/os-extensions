
import WorkingSpecimensReport from './WorkingSpecimensReport.vue';
import WorkingOrderReport from './WorkingOrderReport.vue';

export default {
  install(app, { osSvc }) {
    const pluginsReg = osSvc.pluginViews;

    app.component('washuWorkingSpecimensReport', WorkingSpecimensReport);
    const specimensReport = {name: 'washu-working-specimens-report', component: 'washuWorkingSpecimensReport'};
    pluginsReg.registerView('cart-specimens', 'page-header', specimensReport);

    app.component('washuWorkingOrderReport', WorkingOrderReport);
    const orderReport = {name: 'washu-working-order-report', component: 'washuWorkingOrderReport'};
    pluginsReg.registerView('order-detail', 'more-menu', orderReport);
  }
}
