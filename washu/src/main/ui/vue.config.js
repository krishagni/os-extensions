module.exports = {
  chainWebpack: config => {
    config.plugin("copy")
      .use(require.resolve("copy-webpack-plugin"), [[{
        from: "src/i18n",
        to: "i18n"
      }]]);
  }
}
