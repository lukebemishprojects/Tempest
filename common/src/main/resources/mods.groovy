ModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[47,)'

    license = 'BSD-3-Clause'
    issueTrackerUrl = 'https://github.com/lukebemishprojects/Tempest/issues'

    mod {
        modId = this.buildProperties['mod_id']
        displayName = this.buildProperties['mod_name']
        version = this.version
        displayUrl = 'https://github.com/lukebemishprojects/Tempest'

        description = 'Makes weather more unpleasant.'
        authors = [this.buildProperties['mod_author'] as String]

        dependencies {
            mod 'minecraft', {
                //def minor = this.libs.versions.minecraft.split(/\./)[1] as int
                //versionRange = "[${this.libs.versions.minecraft},1.${minor+1}.0)"
                // for 1.20.1, use an exact version
                versionRange = "[1.20.1,1.20.2)"
            }

            onForge {
                forge = ">=${this.libs.versions.forge}"
            }

            onFabric {
                mod 'fabricloader', {
                    versionRange = ">=${this.libs.versions.fabric.loader}"
                }
                mod 'fabric-api', {
                    versionRange = ">=${this.libs.versions.fabric.api.split(/\+/)[0]}"
                }
            }
        }

        onFabric {
            entrypoints {
                entrypoint 'main', 'dev.lukebemish.tempest.impl.fabriquilt.ModEntrypoint'
                entrypoint 'cardinal-components', 'dev.lukebemish.tempest.impl.fabriquilt.ComponentRegistration'
            }
        }
    }

    onFabric {
        mixins = [
            'mixin.tempest.json',
            'mixin.fabriquilt.tempest.json'
        ]
        custom = [
            'cardinal-components': [
                'tempest:weather_chunk_data'
            ]
        ]
    }
}
