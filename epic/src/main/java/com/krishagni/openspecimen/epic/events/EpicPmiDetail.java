package com.krishagni.openspecimen.epic.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;

public class EpicPmiDetail
{
    private String mrnValue;
    
    private String newMrnValue;
    
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

		public String getNewMrnValue() {
			return newMrnValue;
		}
		
		public void setNewMrnValue(String newMrnValue) {
			this.newMrnValue = newMrnValue;
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

		public static List<EpicPmiDetail> from(List<PmiDetail> pmi) {
			List<EpicPmiDetail> pmis = new ArrayList<EpicPmiDetail>();
			if(CollectionUtils.isNotEmpty(pmi)){
				for (PmiDetail pmiDetail : pmi) {
					EpicPmiDetail detail = new EpicPmiDetail();
					detail.setMrnValue(pmiDetail.getMrn());
					detail.setSiteName(pmiDetail.getSiteName());
					pmis.add(detail);
				}
			}
			return pmis;
		}

		public static List<PmiDetail> to(List<EpicPmiDetail> pmis, boolean isMrnUpdatable) {
			List<PmiDetail> pmisToReturn = new ArrayList<PmiDetail>();
			if(CollectionUtils.isNotEmpty(pmis)){
				for (EpicPmiDetail epmi : pmis) {
					PmiDetail pmi = new PmiDetail();
					pmi.setMrn(isMrnUpdatable ? epmi.getNewMrnValue() : epmi.getMrnValue());
					pmi.setSiteName(epmi.getSiteName());
					pmisToReturn.add(pmi);
				}
			}
			return pmisToReturn;
		}

}
