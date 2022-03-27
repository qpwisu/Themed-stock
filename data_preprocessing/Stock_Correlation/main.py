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
# # #stockPrice 재 업로드 구하기
# endDay = db.collection(u'day').document(u'dday').get().to_dict()["endDay"]
# gstock.update_stock_price(endDay)
stockP=gstock.read_csv("stockPrice.csv")
#
vi=gstock.read_csv("viPrice.csv")
#stockPrice 업로드
fb.load_stock(stockP)
#HighestCorrelation 구하기
HC = gstock.get_highestCorrelation()
# print(HC)
fb.load_highestCorrelation(HC)
#corrAll 업로드
CA = gstock.get_viAndCorr()
with open('CA.pickle', 'rb') as fr:
    CA_loaded = pickle.load(fr)

fb.load_corrAll(CA_loaded)
# 이름 업로드
name= ",".join(stockP.columns)
fb.load_name(name)
fb.load_endDay(str(stockP.index[-1]))
