package com.krishagni.openspecimen.epic.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class PmiDetail
{
    private String mrnValue;
    private Long siteId;
    private String siteName;
    
    public String getMrnValue()
    {
        return mrnValue;
    }
    
    public void setMrnValue(String mrnValue)
    {
        this.mrnValue = mrnValue;
    }

    
    public Long getSiteId()
    {
        return siteId;
    }

    
    public void setSiteId(Long siteId)
    {
        this.siteId = siteId;
    }

    
    public String getSiteName()
    {
        return siteName;
    }

    
    public void setSiteName(String siteName)
    {
        this.siteName = siteName;
    }

		public static List<PmiDetail> from(List<com.krishagni.catissueplus.core.biospecimen.events.PmiDetail> pmi) {
			List<PmiDetail> pmis = new ArrayList<PmiDetail>();
			if(CollectionUtils.isNotEmpty(pmi)){
				for (com.krishagni.catissueplus.core.biospecimen.events.PmiDetail pmiDetail : pmi) {
					PmiDetail detail = new PmiDetail();
					detail.setMrnValue(pmiDetail.getMrn());
					detail.setSiteName(pmiDetail.getSiteName());
					pmis.add(detail);
				}
			}
			return pmis;
		}

    
      
       

}
