<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item active'>Collections</li>
</#global>
<#include "common-header.ftl">

  <h1>GeoServer Feature Collections</h1>
  <p class="my-4">
    This document lists all the collections available in the Features service.<br/>
  </p>
  
  <div class="row">
    <#list model.collections as collection>
    <div class="col-xs-12 col-md-6 col-lg-4 pb-4">
      <div class="card h-100">
        <div class="card-header">
          <h2><a href="${serviceLink("collections/${collection.id}")}">${collection.id}</a></h2>
        </div>
        <#include "collection_include.ftl">
      </div>
    </div>
    </#list>
  </div>

  <script src="${resourceLink('webresources/ogcapi/features.js')}"></script>
  <input type="hidden" id="maxNumberOfFeaturesForPreview" value="${service.maxNumberOfFeaturesForPreview}"/>

<#include "common-footer.ftl">
