#!/usr/bin/python

import jenkins
import xml.etree.ElementTree as ET
import pprint
import yaml
from time import sleep
from wiremock.constants import Config
from wiremock.client import *

# *** VERY IMPORTANT ***
# DO NOT ADD A TRAILING SLASH / on __admin/
# MUST BE __admin

Config.base_url = 'http://wiremock.router.default.svc.cluster.local/__admin'

def find_id_endpoint(all_mappings, endpoint):

    for mapping in all_mappings.mappings:
        try:
            if endpoint in mapping.request.url_path:
                return mapping.id
        except:
            pass
    return None


def create_job(job_config, server, t):
    xml = ET.fromstring(job_config)
    sp = xml.find('./definition/scriptPath')
    sp.text = 'tests/jobs/%s.groovy' % t['jobname']

    server.create_job(t['jobname'], ET.tostring(xml))


def main():

    pp = pprint.PrettyPrinter(indent=4)
    endpoint_id_mapping = {}
    server = jenkins.Jenkins('http://10.53.252.84:8080', username='admin', password='admin')

    job_config = server.get_job_config("mock")
    all_mappings = Mappings.retrieve_all_mappings()
    with open("tests.yml", 'r') as stream:
        api_responses = yaml.load(stream)

    for t in api_responses['tests']:
        id = endpoint_id_mapping.get(t['endpoint'])
        if not id:
            id = find_id_endpoint(all_mappings, t['endpoint'])
            create_job(job_config, server, t)
            endpoint_id_mapping[t['endpoint']] = id

        mapping = Mappings.retrieve_mapping(id)
        mapping.response.status = t['status']
        mapping.response.body_file_name = t['filename']
        Mappings.update_mapping(mapping)

        next_build_number = server.get_job_info(t['jobname'])['nextBuildNumber']
        server.build_job(t['jobname'])

        while True:
            sleep(10)
            build_info = server.get_build_info(t['jobname'], next_build_number)
            if not build_info['building']:
                print "scenario: %s\nstatus: %d\nfilename: %s" % (t['name'], t['status'], t['filename'])
                pp.pprint(build_info)
                break

if __name__ == '__main__':
    main()
