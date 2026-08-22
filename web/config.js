/*
 * Configuração de ambiente do frontend.
 *
 * Define a URL da API consumida pelo TaskFlow. Em desenvolvimento (Live Server),
 * o frontend roda em http://localhost:5501 (ou 127.0.0.1:5501) e a API em
 * http://localhost:8080. Em produção, mantenha vazio para usar a mesma origem
 * (mesmo domínio) ou aponte para o endereço do backend.
 *
 * Para sobrescrever por ambiente, defina window.TASKFLOW_API_URL antes de este
 * arquivo ser carregado (ex.: em um config.js específico do deploy).
 */
(function () {
  var host = window.location.hostname;
  var defaultUrl = (host === 'localhost' || host === '127.0.0.1') ? 'http://' + host + ':8080' : '';
  window.TASKFLOW_API_URL = window.TASKFLOW_API_URL || defaultUrl;
})();