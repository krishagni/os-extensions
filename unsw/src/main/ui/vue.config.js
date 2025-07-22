module.exports = {
  css: { extract: false },
  
  chainWebpack: config => {
    config.plugin("copy")
      .use(require.resolve("copy-webpack-plugin"), [{patterns: [{
        from: "src/i18n",
        to: "i18n"
      }]}]);
  }
}

