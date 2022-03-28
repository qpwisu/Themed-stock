import GetStockCorrelation
import usefb
from datetime import datetime
import pandas as pd
import json
import pickle
from itertools import islice

gstock = GetStockCorrelation.StockCollection()
fb= usefb.Fb()

#첫번째 업로드
gstock.get_stock_price("20200101", "20220322")
# # # #stockPrice 업데이트하기
# # endDay = db.collection(u'day').document(u'dday').get().to_dict()["endDay"]
# # gstock.update_stock_price(endDay)
stockP=gstock.read_csv("stockPrice.csv")

#stockPrice 업로드
fb.load_stock(stockP)

#HighestCorrelation 구하기
HC = gstock.get_highestCorrelation()
fb.load_highestCorrelation(HC)

# corrAll 업로드
CA = gstock.get_viAndCorr()
fb.load_corrAll(CA)

#sector 구하고 업로드
dic1,dic2=gstock.get_sectors()
fb.load_sectors(dic1,dic2)

# 섹터별 vi, 상관관계 분석
SCA = gstock.get_sectors_viAndCorr(dic1,dic2)
fb.load_sectorsCorrAll(SCA)

# 이름 업로드
name= ",".join(stockP.columns)
fb.load_name(name)
fb.load_endDay(str(stockP.index[-1]))

# with open('CA.pickle', 'rb') as fr:
#     CA_loaded = pickle.load(fr)
# print(CA_loaded[""])
